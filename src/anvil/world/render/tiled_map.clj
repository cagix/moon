(ns anvil.world.render.tiled-map
  (:require [anvil.world :as world]
            [anvil.world.render :as render]
            [clojure.gdx.graphics.color :as color]
            [gdl.context :as c]))

(def ^:private explored-tile-color (color/create 0.5 0.5 0.5 1))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter [explored-tile-corners light-cache light-position]
  #_(reset! do-once false)
  (fn tile-color-setter [_color x y]
    (let [position [(int x) (int y)]
          explored? (get @explored-tile-corners position) ; TODO needs int call ?
          base-color (if explored? explored-tile-color color/black)
          cache-entry (get @light-cache position :not-found)
          blocked? (if (= cache-entry :not-found)
                     (let [blocked? (world/ray-blocked? light-position position)]
                       (swap! light-cache assoc position blocked?)
                       blocked?)
                     cache-entry)]
      #_(when @do-once
          (swap! ray-positions conj position))
      (if blocked?
        (if see-all-tiles? color/white base-color)
        (do (when-not explored?
              (swap! explored-tile-corners assoc (mapv int position) true))
            color/white)))))

(defn-impl render/render-tiled-map [{:keys [cdq.context/explored-tile-corners] :as c}
                                    tiled-map
                                    light-position]
  (c/draw-tiled-map c
                    tiled-map
                    (tile-color-setter explored-tile-corners
                                       (atom {})
                                       light-position)))
