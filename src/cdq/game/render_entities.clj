(ns cdq.game.render-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.utils :as utils]
            [cdq.world :as world]))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (graphics/draw-rectangle x y (:width entity) (:height entity) color)))

(defn do! []
  (let [entities (map deref (:active-entities ctx/world))
        player @ctx/player-eid]
    (doseq [[z-order entities] (utils/sort-by-order (group-by :z-order entities)
                                                    first
                                                    world/render-z-order)
            render! [entity/render-below!
                     entity/render-default!
                     entity/render-above!
                     entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (world/line-of-sight? player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity))
       (catch Throwable t
         (draw-body-rect entity :red)
         (utils/pretty-pst t))))))

; I can create this later after loading all the component namespaces
; just go through the systems
; and see which components are signed up for it
; => I get an overview what is rendered how...
#_(def ^:private entity-render-fns
  {:below {:entity/mouseover? draw-faction-ellipse
           :player-item-on-cursor draw-world-item-if-exists
           :stunned draw-stunned-circle}
   :default {:entity/image draw-image-as-of-body
             :entity/clickable draw-text-when-mouseover-and-text
             :entity/line-render draw-line}
   :above {:npc-sleeping draw-zzzz
           :entity/string-effect draw-text
           :entity/temp-modifier draw-filled-circle-grey}
   :info {:entity/hp draw-hpbar-when-mouseover-and-not-full
          :active-skill draw-skill-image-and-active-effect}})
