package world.bentobox.visit.panels.player;


import org.bukkit.Material;
import org.bukkit.conversations.*;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.function.Consumer;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.database.object.IslandVisitSettings;
import world.bentobox.visit.managers.VisitAddonManager;


/**
 * This class shows how to set up easy panel by using BentoBox PanelBuilder API
 */
public class ConfigurePanel
{
	// ---------------------------------------------------------------------
	// Section: Variables
	// ---------------------------------------------------------------------

	/**
	 * This variable allows to access addon object.
	 */
	private final VisitAddon addon;

	/**
	 * This variable allows to access addon manager object.
	 */
	private final VisitAddonManager manager;

	/**
	 * This variable stores main island which GUI is targeted.
	 */
	private final Island island;

	/**
	 * This variable holds user who opens panel. Without it panel cannot be opened.
	 */
	private final User user;


	// ---------------------------------------------------------------------
	// Section: Internal Constructor
	// ---------------------------------------------------------------------


	/**
	 * This is internal constructor. It is used internally in current class to avoid
	 * creating objects everywhere.
	 * @param addon VisitAddon object.
	 */
	private ConfigurePanel(VisitAddon addon,
		@Nullable Island island,
		User user)
	{
		this.addon = addon;
		this.manager = this.addon.getAddonManager();
		this.island = island;
		this.user = user;
	}


	/**
	 * This method is used to open UserPanel outside this class. It will be much easier
	 * to open panel with single method call then initializing new object.
	 * @param addon VisitAddon object
	 * @param user User who opens panel
	 */
	public static void openPanel(VisitAddon addon,
		@Nullable Island island,
		User user)
	{
		new ConfigurePanel(addon, island, user).build();
	}


	// ---------------------------------------------------------------------
	// Section: Methods
	// ---------------------------------------------------------------------


	/**
	 * Build method manages current panel opening. It uses BentoBox PanelAPI that is easy
	 * to use and users can get nice panels.
	 */
	private void build()
	{
		// PanelBuilder is a BentoBox API that provides ability to easy create Panels.
		PanelBuilder panelBuilder = new PanelBuilder().
			user(this.user).
			name(this.user.getTranslation("visit.gui.player.title.configure")).
			type(Panel.Type.HOPPER);

		if (this.island == null)
		{
			// Nothing to open as user do not have island anymore.
			return;
		}

		IslandVisitSettings settings = this.manager.getIslandVisitSettings(this.island);

		panelBuilder.item(0, this.createValueButton(settings));
		panelBuilder.item(2, this.createOfflineOnlyButton(settings));
		panelBuilder.item(4, this.createEnableButton(this.island));

		// At the end we just call build method that creates and opens panel.
		panelBuilder.build();
	}


	/**
	 * This method creates input button that allows to write any number in chat.
	 * @param settings Settings where this number will be saved.
	 * @return PanelItem button that allows to change payment value.
	 */
	private PanelItem createValueButton(IslandVisitSettings settings)
	{
		String name = this.user.getTranslation("visit.gui.player.button.input.name");
		String description = this.user.getTranslation("visit.gui.player.button.input.description",
			"[value]", Double.toString(settings.getPayment()));
		ItemStack icon = new ItemStack(Material.ANVIL);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) -> {

			this.getNumberInput(number ->
				{
					if (number != null)
					{
						// Null value is passed if user write cancel.
						settings.setPayment(number.doubleValue());
						this.manager.saveSettings(settings);
					}

					this.build();
				},
				this.user.getTranslation("visit.gui.questions.number"));

			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(description).
			icon(icon).
			clickHandler(clickHandler).
			build();
	}


	/**
	 * This method creates toggleable button that allows to switch between only online/
	 * offline button.
	 * @param settings Settings where this number will be saved.
	 * @return PanelItem button.
	 */
	private PanelItem createOfflineOnlyButton(IslandVisitSettings settings)
	{
		String name = this.user.getTranslation("visit.gui.player.button.offline.name");
		String description = this.user.getTranslation("visit.gui.player.button.offline.description",
			"[value]", Boolean.toString(settings.isOfflineVisit()));
		ItemStack icon = settings.isOfflineVisit() ?
			new ItemStack(Material.GREEN_STAINED_GLASS_PANE) :
			new ItemStack(Material.RED_STAINED_GLASS_PANE);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
		{
			settings.setOfflineVisit(!settings.isOfflineVisit());
			this.manager.saveSettings(settings);

			panel.getItems().put(slot, this.createOfflineOnlyButton(settings));

			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(description).
			icon(icon).
			clickHandler(clickHandler).
			build();
	}


	/**
	 * This method creates toggleable button that allows to switch if visiting is enabled
	 * or not.
	 * @return PanelItem button.
	 */
	private PanelItem createEnableButton(Island island)
	{
		boolean isAllowed = island.isAllowed(VisitAddon.ALLOW_VISITS_FLAG);

		String name = this.user.getTranslation("visit.gui.player.button.enabled.name");
		String description = this.user.getTranslation("visit.gui.player.button.enabled.description",
			"[value]", Boolean.toString(isAllowed));
		ItemStack icon = new ItemStack(Material.PUMPKIN_PIE);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
		{
			island.setSettingsFlag(VisitAddon.ALLOW_VISITS_FLAG, !isAllowed);
			panel.getItems().put(slot, this.createEnableButton(island));

			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(description).
			icon(icon).
			clickHandler(clickHandler).
			glow(isAllowed).
			build();
	}


	// ---------------------------------------------------------------------
	// Section: Conversation API
	// ---------------------------------------------------------------------


	/**
	 * This method will close opened gui and writes inputText in chat. After players answers on
	 * inputText in chat, message will trigger consumer and gui will reopen.
	 * @param consumer Consumer that accepts player output text.
	 * @param question Message that will be displayed in chat when player triggers conversion.
	 */
	private void getNumberInput(Consumer<Number> consumer, @NonNull String question)
	{
		final User user = this.user;

		Conversation conversation =
			new ConversationFactory(BentoBox.getInstance()).withFirstPrompt(
				new NumericPrompt()
				{
					/**
					 * Override this method to perform some action with
					 * the user's integer response.
					 *
					 * @param context Context information about the
					 * conversation.
					 * @param input The user's response as a {@link
					 * Number}.
					 * @return The next {@link Prompt} in the prompt
					 * graph.
					 */
					@Override
					protected Prompt acceptValidatedInput(ConversationContext context, Number input)
					{
						// Add answer to consumer.
						consumer.accept(input);
						// Reopen GUI
						ConfigurePanel.this.build();
						// End conversation
						return Prompt.END_OF_CONVERSATION;
					}


					/**
					 * Override this method to do further validation on
					 * the numeric player input after the input has been
					 * determined to actually be a number.
					 *
					 * @param context Context information about the
					 * conversation.
					 * @param input The number the player provided.
					 * @return The validity of the player's input.
					 */
					@Override
					protected boolean isNumberValid(ConversationContext context, Number input)
					{
						return input.doubleValue() >= 0.0 &&
							input.doubleValue() <= Double.MAX_VALUE;
					}


					/**
					 * Optionally override this method to display an
					 * additional message if the user enters an invalid
					 * number.
					 *
					 * @param context Context information about the
					 * conversation.
					 * @param invalidInput The invalid input provided by
					 * the user.
					 * @return A message explaining how to correct the
					 * input.
					 */
					@Override
					protected String getInputNotNumericText(ConversationContext context,
						String invalidInput)
					{
						return ConfigurePanel.this.user.getTranslation("visit.error.not-a-number", "[value]", invalidInput);
					}


					/**
					 * Optionally override this method to display an
					 * additional message if the user enters an invalid
					 * numeric input.
					 *
					 * @param context Context information about the
					 * conversation.
					 * @param invalidInput The invalid input provided by
					 * the user.
					 * @return A message explaining how to correct the
					 * input.
					 */
					@Override
					protected String getFailedValidationText(ConversationContext context,
						Number invalidInput)
					{
						return ConfigurePanel.this.user.getTranslation("visit.error.not-valid-number",
							"[value]", invalidInput.toString(),
							"[min]", Double.toString(0),
							"[max]", Double.toString(Double.MAX_VALUE));
					}


					/**
					 * @see Prompt#getPromptText(ConversationContext)
					 */
					@Override
					public String getPromptText(ConversationContext conversationContext)
					{
						// Close input GUI.
						user.closeInventory();

						// There are no editable message. Just return question.
						return question;
					}
				}).
				withLocalEcho(false).
				// On cancel conversation will be closed.
				withEscapeSequence("cancel").
				// Use null value in consumer to detect if user has abandoned conversation.
				addConversationAbandonedListener(abandonedEvent ->
				{
					if (!abandonedEvent.gracefulExit())
					{
						consumer.accept(null);
					}
				}).
				withPrefix(context -> this.user.getTranslation("visit.gui.questions.prefix")).
				buildConversation(user.getPlayer());

		conversation.begin();
	}
}
