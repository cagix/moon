(ns gdl.app
  (:require [clojure.gdx :as gdx]))

(def exit gdx/exit-app)

(defmacro post-runnable [& exprs]
  `(gdx/post-runnable (fn [] ~@exprs)))
