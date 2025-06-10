(ns gdl.application
  (:import (com.badlogic.gdx Gdx)))

(defn post-runnable! [runnable]
  (.postRunnable Gdx/app runnable))
