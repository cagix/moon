(ns clojure.gdx.app
  (:import (com.badlogic.gdx Application)))

(defn post-runnable! [^Application app runnable]
  (.postRunnable app runnable))
