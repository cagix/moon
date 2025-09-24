(ns cdq.application.create.stage.dev-menu.update-labels
  (:require [cdq.graphics :as graphics]
            [clojure.utils :as utils]))

(def items
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
