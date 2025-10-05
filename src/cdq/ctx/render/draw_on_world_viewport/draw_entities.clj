(ns cdq.ctx.render.draw-on-world-viewport.draw-entities
  (:require [cdq.graphics.draws :as draws]
            [cdq.world.raycaster :as raycaster]
            [clojure.graphics.color :as color]
            [clojure.throwable :as throwable]
            [clojure.utils :as utils]))

(def ^:private render-layers
  (map
   #(update-vals % requiring-resolve)
   '[{:entity/mouseover?     cdq.entity.mouseover.draw/txs
      :stunned               cdq.entity.state.stunned.draw/txs
      :player-item-on-cursor cdq.entity.state.player-item-on-cursor.draw/txs}
     {:entity/clickable      cdq.entity.clickable.draw/txs
      :entity/animation      cdq.entity.animation.draw/txs
      :entity/image          cdq.entity.image.draw/txs
      :entity/line-render    cdq.entity.line-render.draw/txs}
     {:npc-sleeping          cdq.entity.state.npc-sleeping.draw/txs
      :entity/temp-modifier  cdq.entity.temp-modifier.draw/txs
      :entity/string-effect  cdq.entity.string-effect.draw/txs}
     {:entity/stats          cdq.entity.stats.draw/txs
      :active-skill          cdq.entity.state.active-skill.draw/txs}]))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-entity
  [{:keys [ctx/graphics]
    :as ctx}
   entity render-layer]
  (try (do
        (when show-body-bounds?
          (draws/handle! graphics (draw-body-rect (:entity/body entity)
                                                  (if (:body/collides? (:entity/body entity))
                                                    color/white
                                                    color/gray))))
        (doseq [[k v] entity
                :let [draw-fn (get render-layer k)]
                :when draw-fn]
          (draws/handle! graphics (draw-fn v entity ctx))))
       (catch Throwable t
         (draws/handle! graphics (draw-body-rect (:entity/body entity) color/red))
         (throwable/pretty-pst t))))

(defn do!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [entities (map deref (:world/active-entities world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (raycaster/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))
