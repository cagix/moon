# Deployment

xception in thread "main" com.badlogic.gdx.utils.GdxRuntimeException: java.lang.AssertionError: Assert failed: (contains? textures file)
	at com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.<init>(Lwjgl3Application.java:159)
	at clojure.gdx.backends.lwjgl.application$create.invokeStatic(application.clj:4)
	at clojure.gdx.backends.lwjgl.application$create.invoke(application.clj:4)
	at cdq.application$_main.invokeStatic(application.clj:1495)
	at cdq.application$_main.invoke(application.clj:1492)
	at clojure.lang.AFn.applyToHelper(AFn.java:152)
	at clojure.lang.AFn.applyTo(AFn.java:144)
	at cdq.application.main(Unknown Source)


    => Cannot search textures via files
    => need other helper


    * Deploy to windows
    * Add controller supports
    * No User Interface/Items/Numbers/stats
    * Pure gameplay

    * Deactivate dev-menu/dev-actions ? editor ?
