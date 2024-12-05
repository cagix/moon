(ns forge.app
  (:import (com.badlogic.gdx Gdx)))

(defn exit []
  (.exit Gdx/app))

(defmacro post-runnable [& exprs]
  `(.postRunnable Gdx/app [] ~@exprs))
