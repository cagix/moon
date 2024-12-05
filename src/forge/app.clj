(ns forge.app
  (:require [clojure.gdx :as gdx]))

(def exit gdx/exit)

(defmacro post-runnable [& exprs]
  `(gdx/post-runnable (fn [] ~@exprs)))
