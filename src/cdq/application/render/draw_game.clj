(ns cdq.application.render.draw-game
  (:require [cdq.graphics :as graphics]
            [cdq.world :as world]
            [com.badlogic.gdx.graphics.color :as color]
            [cdq.render.draw-on-world-viewport :as draw-on-world-viewport]))

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear! graphics color/black)
  ctx)

(defn- tile-color-setter
  [{:keys [ray-blocked?
           explored-tile-corners
           light-position
           see-all-tiles?
           explored-tile-color
           visible-tile-color
           invisible-tile-color]}]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored?
                         explored-tile-color
                         invisible-tile-color)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (ray-blocked? light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles?
            visible-tile-color
            base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              visible-tile-color))))))

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

(defn- draw-world-map!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/draw-tiled-map! graphics
                            (:world/tiled-map world)
                            (tile-color-setter
                             {:ray-blocked? (partial world/ray-blocked? world)
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (graphics/camera-position graphics)
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)

(defn- draw-on-world-viewport!
  [{:keys [ctx/graphics]
    :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (draw-on-world-viewport/do! ctx)))
  ctx)

(defn do! [ctx]
  (-> ctx
      clear-screen!
      draw-world-map!
      draw-on-world-viewport!))
