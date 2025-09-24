(ns cdq.application.create.reset-stage
  (:require cdq.application.create.ui.dev-menu
            cdq.application.create.ui.action-bar
            cdq.application.create.ui.hp-mana-bar
            cdq.application.create.ui.windows
            cdq.application.create.ui.entity-info
            cdq.application.create.ui.inventory
            cdq.application.create.ui.player-state-draw
            cdq.application.create.ui.message))

(require '[cdq.graphics :as graphics])
(require '[clojure.utils :as utils])

(def ^:private update-labels
  [{:label "elapsed-time"
    :update-fn (fn [ctx]
                 (str (utils/readable-number (:world/elapsed-time (:ctx/world ctx))) " seconds"))
    :icon "images/clock.png"}
   {:label "FPS"
    :update-fn (fn [ctx]
                 (graphics/frames-per-second (:ctx/graphics ctx)))
    :icon "images/fps.png"}
   {:label "Mouseover-entity id"
    :update-fn (fn [{:keys [ctx/world]}]
                 (let [eid (:world/mouseover-eid world)]
                   (when-let [entity (and eid @eid)]
                     (:entity/id entity))))
    :icon "images/mouseover.png"}
   {:label "paused?"
    :update-fn (comp :world/paused? :ctx/world)}
   {:label "GUI"
    :update-fn (fn [{:keys [ctx/graphics]}]
                 (mapv int (:graphics/ui-mouse-position graphics)))}
   {:label "World"
    :update-fn (fn [{:keys [ctx/graphics]}]
                 (mapv int (:graphics/world-mouse-position graphics)))}
   {:label "Zoom"
    :update-fn (fn [ctx]
                 (graphics/camera-zoom (:ctx/graphics ctx)))
    :icon "images/zoom.png"}])

(def ui-actors
  [[cdq.application.create.ui.dev-menu/create {:update-labels update-labels}]
   [cdq.application.create.ui.action-bar/create]
   [cdq.application.create.ui.hp-mana-bar/create {:rahmen-file "images/rahmen.png"
                                                  :rahmenw 150
                                                  :rahmenh 26
                                                  :hpcontent-file "images/hp.png"
                                                  :manacontent-file "images/mana.png"
                                                  :y-mana 80}]
   [cdq.application.create.ui.windows/create [cdq.application.create.ui.entity-info/create
                                              cdq.application.create.ui.inventory/create]]
   [cdq.application.create.ui.player-state-draw/create]
   [cdq.application.create.ui.message/create {:duration-seconds 0.5
                                              :name "player-message"}]])
