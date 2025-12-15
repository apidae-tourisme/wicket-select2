package org.retzlaff.select2;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.IRequestListener;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource.Attributes;
import org.retzlaff.select2.resource.AjaxSearchResource;
import org.retzlaff.select2.resource.AjaxSearchResourceReference;

/**
 * Select2 drop down component that populates its choices with AJAX.
 * <p>
 * This component must be attached to a hidden input field.
 * 
 * @author dan
 * @param <T> model type (E or Collection<E>)
 * @param <E> element type
 */
public abstract class AbstractSelect2Choice<T, E> extends HiddenField<T> implements IRequestListener {

	private static final long serialVersionUID = 1L;

	private final Select2Behavior<T, E> behavior;
	
	private ISelect2AjaxAdapter<E> adapter;
	
	private AjaxSearchResourceReference<E> resourceReference;
	
	/**
	 * Constructor.
	 * 
	 * @param id
	 * @param adapter
	 */
	protected AbstractSelect2Choice(String id, IModel<T> model, AjaxSearchResourceReference<E> resourceReference, boolean multiple) {
		super(id, model);
		this.behavior = new Select2Behavior<T, E>(multiple, this);
		this.resourceReference = resourceReference;
		add(behavior);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param id
	 * @param adapter
	 */
	protected AbstractSelect2Choice(String id, IModel<T> model, ISelect2AjaxAdapter<E> adapter, boolean multiple) {
		super(id, model);
		this.behavior = new Select2Behavior<T, E>(multiple, this);
		this.adapter = adapter;
		add(behavior);
	}
	
	// accessors

	public void setSettings(Select2Settings settings) {
		behavior.setSettings(settings);
	}
	public Select2Settings getSettings() {
		return behavior.getSettings();
	}
	
	/**
	 * Use a {@link AjaxSearchResourceReference} instead. You will avoid the overhead of page loading at each request
	 * (which is synchronized for each user session)
	 */
	@Deprecated
	public void setAdapter(ISelect2AjaxAdapter<E> adapter) {
		if (resourceReference != null && adapter != null) {
			throw new IllegalStateException("Cannot call setAdapter() if a resourceReference was set");
		}
		this.adapter = adapter;
	}
	
	public ISelect2AjaxAdapter<E> getAdapter() {
		if (resourceReference != null) {
			return resourceReference.getAdapter();
		} else {
			return adapter;
		}
	}
	
	public void setResourceReference(AjaxSearchResourceReference<E> resourceReference) {
		if (resourceReference != null && adapter != null) {
			throw new IllegalStateException("Cannot call setResourceReference() if an adapter was set");
		}
		this.resourceReference = resourceReference;
	}
	
	public AjaxSearchResourceReference<E> getResourceReference() {
		return resourceReference;
	}
	
	public CharSequence getAjaxUrl() {
		if (resourceReference != null) {
			if (resourceReference.canBeRegistered()) {
				getApplication().getResourceReferenceRegistry().registerResourceReference(resourceReference);
			}
			return urlFor(resourceReference, getAjaxParameters());
		} else {
			return urlForListener(getAjaxParameters());
		}
	}
	
	protected PageParameters getAjaxParameters() {
		return new PageParameters();
	}
	
	/**
	 * Returns a non-null list of currently selected objects.
	 */
	protected abstract Collection<E> getModelObjects();
	
	@Override
	public void onRequest() {
		RequestCycle rc = RequestCycle.get();
		Attributes a = new Attributes(rc.getRequest(), rc.getResponse(), null);
		new AjaxChoiceResource().respond(a);
	}
	
	private final class AjaxChoiceResource extends AjaxSearchResource<E> {
		private static final long serialVersionUID = 1L;
		
		private transient IChoiceRenderer<? super E> choiceRendererCache;

		public AjaxChoiceResource() {
			super(null);
			setTextEncoding(getSettings().getTextEncoding());
			setAjaxCacheDuration(getSettings().getAjaxCacheDuration());
			setPageLimit(getSettings().getPageLimit());
		}
		
		@Override
		protected IChoiceRenderer<? super E> getChoiceRenderer(IRequestParameters params) {
			/* Don't care about synchronization here: worst case, the choiceRenderer gets erased
			 * with another object that acts exactly the same)
			 */
			if (choiceRendererCache == null) {
				choiceRendererCache = new ChoiceRenderer<E>() {
					private static final long serialVersionUID = 1L;

					@Override
					public Object getDisplayValue(E object) {
						return AbstractSelect2Choice.this.getAdapter().getDisplayValue(object);
					}

					@Override
					public String getIdValue(E object, int index) {
						return AbstractSelect2Choice.this.getAdapter().getChoiceId(object);
					}
				};
			}
			return choiceRendererCache;
		}

		@Override
		public List<E> getChoices(IRequestParameters params, int start, int count, String filter) {
			return getAdapter().getChoices(start, count, filter);
		}
	}
}
