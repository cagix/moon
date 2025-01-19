(ns cdq.render.draw-on-world-view
  (:require [cdq.graphics.camera :as camera]
            [cdq.graphics.color :as color]
            [cdq.graphics.shape-drawer :as sd]
            [cdq.graphics.2d.batch :as batch]
            [cdq.utils :as utils]))

(def render-fns
  '[(cdq.render.before-entities/render)
    (cdq.world.graphics/render-entities
     {:below {:entity/mouseover? cdq.world.graphics/draw-faction-ellipse
              :player-item-on-cursor cdq.world.graphics/draw-world-item-if-exists
              :stunned cdq.world.graphics/draw-stunned-circle}
      :default {:entity/image cdq.world.graphics/draw-image-as-of-body
                :entity/clickable cdq.world.graphics/draw-text-when-mouseover-and-text
                :entity/line-render cdq.world.graphics/draw-line}
      :above {:npc-sleeping cdq.world.graphics/draw-zzzz
              :entity/string-effect cdq.world.graphics/draw-text
              :entity/temp-modifier cdq.world.graphics/draw-filled-circle-grey}
      :info {:entity/hp cdq.world.graphics/draw-hpbar-when-mouseover-and-not-full
             :active-skill cdq.world.graphics/draw-skill-image-and-active-effect}})
    (cdq.render.after-entities/render)])

(defn- draw-with [{:keys [cdq.graphics/batch
                          cdq.graphics/shape-drawer] :as c}
                 viewport
                 unit-scale
                 draw-fn]
  (batch/set-color batch color/white) ; fix scene2d.ui.tooltip flickering
  (batch/set-projection-matrix batch (camera/combined (:camera viewport)))
  (batch/begin batch)
  (sd/with-line-width shape-drawer unit-scale
    (fn []
      (draw-fn (assoc c :cdq.context/unit-scale unit-scale))))
  (batch/end batch))

(defn- draw-on-world-view* [{:keys [cdq.graphics/world-unit-scale
                                    cdq.graphics/world-viewport] :as c} render-fn]
  (draw-with c
             world-viewport
             world-unit-scale
             render-fn))

(defn render [context]
  (draw-on-world-view* context
                       (fn [context]
                         (doseq [f render-fns]
                           (utils/req-resolve-call f context))))
  context)
