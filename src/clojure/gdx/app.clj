(ns clojure.gdx.app
  (:import (com.badlogic.gdx Application)))

(defn post-runnable! [^Application application runnable]
  (.postRunnable application runnable))
