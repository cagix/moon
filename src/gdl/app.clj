(ns gdl.app)

(defn post-runnable [runnable]
  (.postRunnable com.badlogic.gdx.Gdx/app runnable))
