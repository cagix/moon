(ns cdq.ctx.stage
  (:require [clojure.gdx.scenes.scene2d.actor :as actor]))

(defprotocol Stage
  (toggle-inventory-visible! [stage])
  (show-modal-window! [stage ui-viewport {:keys [title text button-text on-click]}])
  (set-item!  [stage cell item-properties])
  (remove-item!  [stage inventory-cell])
  (add-skill!  [stage skill-properties])
  (remove-skill!  [stage skill-id])
  (show-text-message!  [stage message]))

(defn viewport-width  [stage] (:viewport/width  (.getViewport stage)))
(defn viewport-height [stage] (:viewport/height (.getViewport stage)))

(defn inventory-window-visible? [stage]
  (-> stage :windows :inventory-window actor/visible?))
