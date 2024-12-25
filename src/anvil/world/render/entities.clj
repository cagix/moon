(ns anvil.world.render.entities
  (:require [anvil.component :as component]
            [anvil.world :as world :refer [line-of-sight?]]
            [anvil.world.render :as render]
            [gdl.context :as c]))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [c entity color]
  (let [[x y] (:left-bottom entity)]
    (c/rectangle c x y (:width entity) (:height entity) color)))

(defn- render-entity! [c system entity]
  (try
   (when show-body-bounds
     (draw-body-rect c entity (if (:collides? entity) :white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect c entity :red)
     (pretty-pst t))))

(defn-impl render/entities [entities]
  (let [c (c/get-ctx)
        player @world/player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              world/render-z-order)
            system [component/render-below
                    component/render-default
                    component/render-above
                    component/render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! c system entity))))
