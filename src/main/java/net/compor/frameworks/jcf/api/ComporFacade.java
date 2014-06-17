package net.compor.frameworks.jcf.api;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import net.compor.frameworks.jcf.ComporType;
import net.compor.frameworks.jcf.ExecutionScript;
import net.compor.frameworks.jcf.ProvidedService;
import net.compor.frameworks.jcf.ScriptContainer;
import net.compor.frameworks.jcf.ServiceRequest;
import net.compor.frameworks.jcf.ServiceResponse;
import net.compor.frameworks.jcf.ServiceSpecification;

public class ComporFacade {

	protected ExecutionScript compor;
	private ScriptContainer container;

	public ComporFacade() {
		container = new ScriptContainer("container");
		compor = new ExecutionScript(container);
		compor.init();
		addComponents();
	}
	
	protected void add(Component component) {
		boolean hasServices = false;
		
		for (Method method : component.getClass().getMethods()) {
			Service annotation = method.getAnnotation(Service.class);
			if (annotation != null) {
				addService(component, method, annotation);
				hasServices = true;
			}
		}
		
		if (hasServices) {
			container.addComponent(component);
			component.start();
		} else {
			throw new RuntimeException("Component without services");
		}
	}

	private void addService(Component component, Method method, Service annotation) {
		String name = (annotation.name().equals(Component.USE_METHOD_NAME)) ? 
				method.getName() : annotation.name();
		String description = (annotation.description().equals(Component.USE_METHOD_NAME)) ? 
				method.getName() : annotation.description();
				
		List<Class<?>> serviceParameters = Arrays.asList(method.getParameterTypes());
		Class<?> returnType = method.getReturnType();
		ComporType serviceReturn = new ComporType(returnType, returnType.getCanonicalName());

		ServiceSpecification specification = 
				new ServiceSpecification(name, description, serviceParameters, null, serviceReturn);
		
		ProvidedService providedService = new ProvidedService(specification, method);
		component.publishService(providedService);
		
	}

	protected void addComponents() {
	}

	protected Object requestService(String service, Object... arguments) {
		try {
			ServiceRequest request = new ServiceRequest(service, arguments);
			ServiceResponse response = compor.exec(request);
	
			if (response.hasException()) {
				Throwable exception = response.getException();
				throw exceptionToThrow(exception);
				
			} else {
				return response.getData();
			}
			
		} catch (Throwable e) {
			throw exceptionToThrow(e);
		}
	}

	private RuntimeException exceptionToThrow(Throwable exception) {
		if (exception instanceof RuntimeException) {
			return (RuntimeException) exception;
		} else {
			return new RuntimeException(exception);
		}
	}

}
