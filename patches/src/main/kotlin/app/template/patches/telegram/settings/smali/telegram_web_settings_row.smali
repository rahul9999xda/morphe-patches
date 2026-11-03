# Insert immediately before return-void in
# Lorg/telegram/ui/t91;->b0(Lorg/telegram/ui/t91;Ljava/util/ArrayList;)V
# Verified target-method local: v1 already contains the Settings ArrayList.

    sget-object v0, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
    invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
    move-result-object v0

    new-instance v2, Lorg/telegram/ui/Components/r71;
    const/4 v3, -0x4
    invoke-direct {v2, v3}, Lorg/telegram/ui/Components/r71;-><init>(I)V
    iput-object v0, v2, Lorg/telegram/ui/Components/r71;->c:Landroid/view/View;

    invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->getRowHeightPx(Landroid/content/Context;)I
    move-result v3
    iput v3, v2, Lorg/telegram/ui/Components/r71;->z:I

    # Keep v1 (the ArrayList) intact. Ignore ArrayList.add's boolean return.
    invoke-virtual {v1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
