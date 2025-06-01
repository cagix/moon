(ns gdl.create.input
  (:require [clojure.gdx :as gdx]))

(defn do! [ctx]
  (assoc ctx :ctx/input (gdx/input)))
