# Correct injection for Telegram Web 13.1.1
# Insert immediately before return-void in:
# Lorg/telegram/ui/t91;->b0(Lorg/telegram/ui/t91;Ljava/util/ArrayList;)V
# At this point v1 is the ArrayList and v0/v2/v3 are scratch registers.

    sget-object v0, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
    invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
    move-result-object v0

    # g71 is the settings-list model object. Its l(View) factory creates
    # the correct -4 custom-view item and stores the View in field c.
    invoke-static {v0}, Lorg/telegram/ui/Components/g71;->l(Landroid/view/View;)Lorg/telegram/ui/Components/g71;
    move-result-object v2

    # Set the requested row height on g71 (not on r71, which is a View).
    invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->getRowHeightPx(Landroid/content/Context;)I
    move-result v3
    iput v3, v2, Lorg/telegram/ui/Components/g71;->z:I

    # Append the g71 item to the existing settings ArrayList.
    invoke-virtual {v1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
