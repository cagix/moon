(ns cdq.create.assoc-gdx
  (:require [clojure.gdx :as gdx]))

(defn do! [ctx]
  (assoc ctx :ctx/gdx (gdx/state)))
