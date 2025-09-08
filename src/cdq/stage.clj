(ns cdq.stage
  (:require [clojure.gdx.scenes.scene2d.actor :as actor]))

(defn toggle-inventory-visible! [stage]
  (-> stage :windows :inventory-window actor/toggle-visible!))
