package world.bentobox.visit.panels.admin;


import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.conversations.*;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.visit.VisitAddon;
import world.bentobox.visit.managers.VisitAddonManager;
import world.bentobox.visit.panels.GuiUtils;
import world.bentobox.visit.panels.player.ConfigurePanel;


/**
 * This class allows to edit default values as well as manage other player island values
 * from single GUI.
 */
public class AdminPanel
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
	 * This variable stores main world where GUI is targeted.
	 */
	private final World world;

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
	private AdminPanel(VisitAddon addon,
		World world,
		User user)
	{
		this.addon = addon;
		this.manager = this.addon.getAddonManager();
		this.world = world;
		this.user = user;
	}


	/**
	 * This method is used to open UserPanel outside this class. It will be much easier
	 * to open panel with single method call then initializing new object.
	 * @param addon VisitAddon object
	 * @param user User who opens panel
	 */
	public static void openPanel(VisitAddon addon,
		World world,
		User user)
	{
		new AdminPanel(addon, world, user).build();
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
			name(this.user.getTranslation("visit.gui.admin.title.main"));

		// Fill border
		GuiUtils.fillBorder(panelBuilder, 4, Material.MAGENTA_STAINED_GLASS_PANE);

		panelBuilder.item(10, this.createManageIslandButton());
		panelBuilder.item(19, this.createDeleteAllButton());

		panelBuilder.item(12, this.createTaxButton());
		panelBuilder.item(21, this.createHeaderButton());

		panelBuilder.item(14, this.createDefaultPaymentButton());
		panelBuilder.item(15, this.createDefaultOfflineButton());
		panelBuilder.item(16, this.createDefaultEnableButton());

		// At the end we just call build method that creates and opens panel.
		panelBuilder.build();
	}


	/**
	 * This method creates button that allows to manage any island settings.
	 * @return PanelItem button.
	 */
	private PanelItem createManageIslandButton()
	{
		String name = this.user.getTranslation("visit.gui.admin.button.manage.name");
		String description = this.user.getTranslation("visit.gui.admin.button.manage.description");
		ItemStack icon = new ItemStack(Material.PLAYER_HEAD);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
		{
			List<Island> islandList = this.addon.getIslands().getIslands(this.world).stream().
				// Filter out islands without owner.
				filter(Island::isOwned).
				// Sort by island and owner name
				sorted((o1, o2) ->
					(o1.getName() != null ?
						o1.getName() : Objects.requireNonNull(User.getInstance(o1.getOwner())).getName()).
						compareToIgnoreCase(o2.getName() != null ?
							o2.getName() : Objects.requireNonNull(User.getInstance(o2.getOwner())).getName())).
					collect(Collectors.toList());

			// Open Edit panel after user selected island.
			SelectIslandPanel.open(user,
				islandList,
				island -> ConfigurePanel.openPanel(this.addon, island, this.user));

			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(GuiUtils.stringSplit(description)).
			icon(icon).
			clickHandler(clickHandler).
			build();
	}


	/**
	 * This method creates button that allows to reset every island to default values.
	 * @return PanelItem button.
	 */
	private PanelItem createDeleteAllButton()
	{
		String name = this.user.getTranslation("visit.gui.admin.button.reset.name");
		String description = this.user.getTranslation("visit.gui.admin.button.reset.description");
		ItemStack icon = new ItemStack(Material.TNT);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
		{
			this.addon.getAddonManager().wipeDatabase();
			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(GuiUtils.stringSplit(description)).
			icon(icon).
			clickHandler(clickHandler).
			build();
	}
	

	/**
	 * This method creates button that starts ConversationAPI to get amount for tax value.
	 * @return PanelItem button.
	 */
	private PanelItem createTaxButton()
	{
		String name = this.user.getTranslation("visit.gui.admin.button.tax.name");
		String description = this.user.getTranslation("visit.gui.admin.button.tax.description",
			"[value]", Double.toString(this.addon.getSettings().getTaxAmount()));
		ItemStack icon = new ItemStack(Material.GOLD_INGOT);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) -> {

			this.getNumberInput(number ->
				{
					if (number != null)
					{
						// Null value is passed if user write cancel.
						this.addon.getSettings().setTaxAmount(number.doubleValue());
						this.addon.saveSettings();
					}

					this.build();
				},
				this.user.getTranslation("visit.gui.questions.tax-number"));

			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(GuiUtils.stringSplit(description)).
			icon(icon).
			clickHandler(clickHandler).
			build();
	}


	/**
	 * This method creates toggleable button that allows to enable and disable gamemode
	 * selection header in player visit panel.
	 * @return PanelItem button.
	 */
	private PanelItem createHeaderButton()
	{
		boolean isAllowed = this.addon.getSettings().isShowGameModeHeader();

		String name = this.user.getTranslation("visit.gui.admin.button.gamemode-header.name");
		String description = this.user.getTranslation("visit.gui.admin.button.gamemode-header.description",
			"[value]", Boolean.toString(isAllowed));
		ItemStack icon = new ItemStack(Material.MUSIC_DISC_11);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
		{
			this.addon.getSettings().setShowGameModeHeader(!isAllowed);
			this.addon.saveSettings();

			this.build();

			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(GuiUtils.stringSplit(description)).
			icon(icon).
			clickHandler(clickHandler).
			glow(isAllowed).
			build();
	}


	/**
	 * This method creates button that starts ConversationAPI to get amount for
	 * default payment value.
	 * @return PanelItem button.
	 */
	private PanelItem createDefaultPaymentButton()
	{
		String name = this.user.getTranslation("visit.gui.admin.button.default-payment.name");
		String description = this.user.getTranslation("visit.gui.admin.button.default-payment.description",
			"[value]", Double.toString(this.addon.getSettings().getDefaultVisitingPayment()));
		ItemStack icon = new ItemStack(Material.ANVIL);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) -> {

			this.getNumberInput(number ->
				{
					if (number != null)
					{
						// Null value is passed if user write cancel.
						this.addon.getSettings().setDefaultVisitingPayment(number.doubleValue());
						this.addon.saveSettings();
					}

					this.build();
				},
				this.user.getTranslation("visit.gui.questions.number"));

			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(GuiUtils.stringSplit(description)).
			icon(icon).
			clickHandler(clickHandler).
			build();
	}


	/**
	 * This method creates toggleable button that allows to switch if default offline
	 * visiting is enabled or not.
	 * @return PanelItem button.
	 */
	private PanelItem createDefaultOfflineButton()
	{
		boolean isAllowed = this.addon.getSettings().isDefaultVisitingOffline();

		String name = this.user.getTranslation("visit.gui.admin.button.default-offline.name");
		String description = this.user.getTranslation("visit.gui.admin.button.default-offline.description",
			"[value]", Boolean.toString(isAllowed));
		ItemStack icon = new ItemStack(Material.MUSIC_DISC_11);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
		{
			this.addon.getSettings().setDefaultVisitingOffline(!isAllowed);
			this.addon.saveSettings();

			this.build();

			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(GuiUtils.stringSplit(description)).
			icon(icon).
			clickHandler(clickHandler).
			glow(isAllowed).
			build();
	}

	
	/**
	 * This method creates toggleable button that allows to switch if default visiting is 
	 * enabled or not.
	 * @return PanelItem button.
	 */
	private PanelItem createDefaultEnableButton()
	{
		boolean isAllowed = this.addon.getSettings().isDefaultVisitingEnabled();

		String name = this.user.getTranslation("visit.gui.admin.button.default-visiting.name");
		String description = this.user.getTranslation("visit.gui.admin.button.default-visiting.description",
			"[value]", Boolean.toString(isAllowed));
		ItemStack icon = new ItemStack(Material.MINECART);
		PanelItem.ClickHandler clickHandler = (panel, user, clickType, slot) ->
		{
			this.addon.getSettings().setDefaultVisitingEnabled(!isAllowed);
			VisitAddon.ALLOW_VISITS_FLAG.setDefaultSetting(!isAllowed);
			this.addon.saveSettings();

			this.build();

			return true;
		};

		return new PanelItemBuilder().
			name(name).
			description(GuiUtils.stringSplit(description)).
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
						AdminPanel.this.build();
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
						return AdminPanel.this.user.getTranslation("visit.error.not-a-number", "[value]", invalidInput);
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
						return AdminPanel.this.user.getTranslation("visit.error.not-valid-number",
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

