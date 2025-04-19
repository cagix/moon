(ns cdq.render.tiled-map
  (:require [cdq.graphics.tiled-map-renderer :as tiled-map-renderer]
            [cdq.graphics.camera :as camera]
            [cdq.math.raycaster :as raycaster])
  (:import (com.badlogic.gdx.graphics Color)))

(def ^:private explored-tile-color (Color. (float 0.5) (float 0.5) (float 0.5) (float 1)))

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

(defn- tile-color-setter [raycaster explored-tile-corners light-position]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored? explored-tile-color Color/BLACK)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? Color/WHITE base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              Color/WHITE))))))

(defn render [{:keys [cdq.graphics/world-viewport
                      cdq.context/tiled-map
                      cdq.context/raycaster
                      cdq.context/explored-tile-corners]
               :as context}]
  (tiled-map-renderer/draw context
                           tiled-map
                           (tile-color-setter raycaster
                                              explored-tile-corners
                                              (camera/position (:camera world-viewport))))
  context)
