package org.retzlaff.select2;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.IResourceListener;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.AbstractChoice;
import org.apache.wicket.markup.html.form.AbstractSingleSelectChoice;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.retzlaff.select2.resource.Select2CssResourceReference;
import org.retzlaff.select2.resource.Select2JavaScriptResourceReference;

/**
 * Select2 drop down decorator
 * <p>
 * See http://ivaynberg.github.com/select2/
 * 
 * @param <T> model type of component (E or Collection<E>)
 * @param <E> element type
 * @author dan
 */
public class Select2Behavior<T, E> extends Behavior {
	private static final long serialVersionUID = -8207277228470597423L;
	
	/**
	 * Holds the settings
	 */
	private Select2Settings settings = new Select2Settings();
	
	/**
	 * Is this a multiple select?
	 */
	private final boolean multiple;

	/**
	 * If behavior is attached to an {@link AbstractSelect2Choice}, choices will be provided with AJAX.
	 */
	private final AbstractSelect2Choice<T, E> ajaxField;
	
	/**
	 * If behavior is attached to an {@link AbstractChoice}, its choices and renderer will be used.
	 */
	private AbstractChoice<T, E> choiceField;
	
	/**
	 * Constructor
	 * 
	 * @param multiple
	 * @see Select2Behavior#forChoice()
	 */
	public Select2Behavior(boolean multiple) {
		this(multiple, null);
	}
	
	Select2Behavior(boolean multiple, AbstractSelect2Choice<T, E> ajaxField) {
		this.multiple = multiple;
		this.ajaxField = ajaxField;
	}
	
	// accessors
	
	public void setSettings(Select2Settings settings) {
		this.settings = settings;
	}
	public Select2Settings getSettings() {
		return settings;
	}
	
	protected List<? extends E> getStaticChoices() {
		if (choiceField != null) {
			return choiceField.getChoices();
		}
		return null;
	}
	
	protected IChoiceRenderer<? super E> getChoiceRenderer() {
		if (choiceField != null) {
			return choiceField.getChoiceRenderer();
		}
		
		return new IChoiceRenderer<E>() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public String getIdValue(E object, int index) {
				return ajaxField.getAdapter().getChoiceId(object);
			}
			
			@Override
			public Object getDisplayValue(E object) {
				return ajaxField.getAdapter().getDisplayValue(object);
			}
		};
	}
	
	// behavior overrides

	@Override
	@SuppressWarnings("unchecked")
	public void bind(Component component) {
		if (component instanceof AbstractChoice) {
			this.choiceField = (AbstractChoice<T, E>) component;
		}
		else if (ajaxField == null) {
			throw new IllegalStateException("select2 behavior requires a choice component, not " +
					component.getClass().getName());
		}
		component.setOutputMarkupId(true);
	}
	
	@Override
	public void renderHead(Component component, IHeaderResponse response) {
		AbstractSingleSelectChoice<?> singleSelectChoice = null;
		if (component instanceof AbstractSingleSelectChoice<?>) {
			singleSelectChoice = (AbstractSingleSelectChoice<?>) component;
			if (!singleSelectChoice.isNullValid()) {
				if (settings.getPlaceholderKey() == null) {
					settings.setPlaceholderKey("null");
				}
				singleSelectChoice.setNullValid(true);
			}
		}
		
		response.render(CssHeaderItem.forReference(Select2CssResourceReference.get()));
		response.render(JavaScriptHeaderItem.forReference(Select2JavaScriptResourceReference.get()));
		
		StringBuilder opts = new StringBuilder();
		
		if (multiple && component instanceof HiddenField) {
			opts.append("multiple:true,");
		}
		
		if (settings.getWidth() != null) {
			opts.append("width:").append(escape(settings.getWidth())).append(',');
		}
		
		if (settings.getMinimumInputLength() > 0) {
			opts.append("minimumInputLength:").append(settings.getMinimumInputLength()).append(',');
		}
		
		if (settings.getMinimumResultsForSearch() > 0) {
			opts.append("minimumResultsForSearch:").append(settings.getMinimumResultsForSearch()).append(',');
		}
		
		if (multiple && settings.getMaximumSelectionSize() > 0) {
			opts.append("maximumSelectionSize:").append(settings.getMaximumSelectionSize()).append(',');
		}
		
		boolean hasPlaceholder = false;
		if (settings.getPlaceholderKey() != null) {
			String msg = component.getLocalizer().getStringIgnoreSettings(settings.getPlaceholderKey(), component, null, null);
			opts.append("placeholder:").append(escape(msg)).append(",");
			hasPlaceholder = true;
		} else if (settings.isAllowClear()) {
			opts.append("placeholder:").append(escape(" ")).append(",");
			hasPlaceholder = true;
		}
		
		if (!settings.getContainerCss().isEmpty()) {
			opts.append("containerCss: { ");
			appendMap(opts, settings.getContainerCss());
			opts.append("},");
		}
		
		if (settings.getContainerCssClass() != null) {
			opts.append("containerCssClass: ").append(escape(settings.getContainerCssClass())).append(",");
		}
		
		if (!settings.getDropdownCss().isEmpty()) {
			opts.append("dropdownCss: { ");
			appendMap(opts, settings.getDropdownCss());
			opts.append("},");
		}
		
		if (settings.getDropdownCssClass() != null) {
			opts.append("dropdownCssClass: ").append(escape(settings.getDropdownCssClass())).append(",");
		}

		if (settings.isAllowClear()) {
			opts.append("allowClear:true,");
		}
		
		if (settings.getNoMatchesKey() != null) {
			String msg = component.getLocalizer().getStringIgnoreSettings(settings.getNoMatchesKey(), component, null, null);
			opts.append("formatNoMatches:function(){return ").append(escape(msg)).append(";},");
		}
		
		if (settings.getInputTooShortKey() != null) {
			String msg = component.getLocalizer().getStringIgnoreSettings(settings.getInputTooShortKey(), component, 
					Model.of(settings.getMinimumInputLength()), null);
			opts.append("formatInputTooShort:function(){return ").append(escape(msg)).append(";},");
		}
		
		if (settings.getSearchingKey() != null) {
			String msg = component.getLocalizer().getStringIgnoreSettings(settings.getSearchingKey(), component, null, null);
			opts.append("formatSearching:function(){return ").append(escape(msg)).append(";},");
		}
		
		if (multiple && settings.getSelectionTooBigKey() != null) {
			String msg = component.getLocalizer().getStringIgnoreSettings(settings.getSelectionTooBigKey(), component, 
					Model.of(settings.getMaximumSelectionSize()), null);
			opts.append("formatSelectionTooBig:function(){return ").append(escape(msg)).append(";},");
		}
		
		String matcher = settings.getMatcher();
		if (matcher != null) {
			opts.append("matcher: ").append(matcher).append(",");
		}
		
		String formatResult = settings.getFormatResult();
		if (formatResult != null) {
			opts.append("formatResult: ").append(formatResult).append(",");
		}
		
		String createSearchChoice = settings.getCreateSearchChoice();
		if (createSearchChoice != null) {
			opts.append("createSearchChoice: ").append(createSearchChoice).append(",");
		}

		if (ajaxField != null) {
			opts.append("ajax:{");
			opts.append("url:'").append(ajaxField.urlFor(IResourceListener.INTERFACE, new PageParameters())).append("',");
			opts.append("dataType:'jsonp',");
			if (settings.getQuietMillis() > 0) {
				opts.append("quietMillis:").append(settings.getQuietMillis()).append(',');
			}
			opts.append("data:function(q,page){return {q:q,page:page};},");
			opts.append("results:function(data,page){return data;}");
			opts.append("},");
			
			if (!ajaxField.getModelObjects().isEmpty()) {
				opts.append("initSelection:function(e, callback){callback( ");
				if (multiple) {
					opts.append('[');
				}
				for (E object : ajaxField.getModelObjects()) {
					String id = ajaxField.getAdapter().getChoiceId(object);
					Object value = ajaxField.getAdapter().getDisplayValue(object);
					opts.append("{id:").append(escape(id)).append(",text:").append(escape(value)).append("},");
				}
				opts.setLength(opts.length() - 1); // trailing comma
				if (multiple) {
					opts.append(']');
				}
				opts.append(");},");
			}
			
			if (createSearchChoice == null && settings.isTagging()) {
				opts.append("createSearchChoice:function(q){return {id:q,text:q};},");
			}
		}
		
		StringBuilder js = new StringBuilder(opts.length() + 32);
		js.append("$('#").append(component.getMarkupId()).append("').select2(");
		if (opts.length() > 0) {
			opts.setLength(opts.length() - 1); // trailing comma
			js.append('{').append(opts).append('}');
		}
		js.append(");");
		
		// HACK: remove the null-value option from AbstractSingleSelectChoice#getDefaultChoice().
		// Select2 has its own way of handling null.
		if (!hasPlaceholder && choiceField instanceof AbstractSingleSelectChoice && "".equals(choiceField.getValue())) {
			js.append("$('#").append(component.getMarkupId()).append(" option[value=\"\"]').remove()");
		}
		
		response.render(OnDomReadyHeaderItem.forScript(js.toString()));
	}
	
	@Override
	public void onComponentTag(Component component, ComponentTag tag) {
		if (multiple && !(component instanceof HiddenField)) {
			tag.put("multiple", "");
		}
	}

	/**
	 * Formats choices in JSON using the choice renderer
	 */
	CharSequence renderChoices(List<? extends E> choices, int index) {
		if (choices == null || choices.isEmpty()) {
			return "[]";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		
		IChoiceRenderer<? super E> r = getChoiceRenderer();
		for (E object : choices) {
			String id = r.getIdValue(object, index++);
			Object value = r.getDisplayValue(object);
			
			sb.append("{id:").append(escape(id)).append(",text:").append(escape(value)).append("},");
		}
		
		sb.setLength(sb.length() - 1); // trailing comma
		sb.append(']');
		return sb;
	}
	
	private CharSequence escape(Object object) {
		return "'" + object.toString().replace("'", "\\'") + "'";
	}
	
	private void appendMap(StringBuilder sb, Map<?, ?> map) {
		Set<?> keySet = map.keySet();
		Iterator<?> it = keySet.iterator();
		while (it.hasNext()) {
			final Object key = it.next();
			sb.append(escape(key))
				.append(": ")
				.append(escape(map.get(key)));
			if (it.hasNext()) {
				sb.append(",");
			}
		}
	}
	
	/**
	 * Factory method that uses type inference to make code shorter.
	 */
	public static <T, E> Select2Behavior<T, E> forChoice(DropDownChoice<T> choice) {
		return new Select2Behavior<T, E>(false);
	}
	
	/**
	 * Factory method that uses type inference to make code shorter.
	 */
	public static <T, E> Select2Behavior<T, E> forChoice(ListMultipleChoice<T> choice) {
		return new Select2Behavior<T, E>(true);
	}
}
