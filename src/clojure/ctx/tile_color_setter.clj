(ns clojure.ctx.tile-color-setter
  (:require [clojure.gdx :as gdx]
            [clojure.graphics.camera :as camera]
            [clojure.raycaster :as raycaster]))

(def see-all-tiles? false)

(def explored-tile-color (gdx/->color [0.5 0.5 0.5 1]))

(def white (gdx/->color :white))
(def black (gdx/->color :black))

(defn tile-color-setter [{:keys [ctx/raycaster
                                 ctx/explored-tile-corners
                                 ctx/world-viewport]}]
  #_(reset! do-once false)
  (let [light-position (camera/position (:camera world-viewport))
        light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored?
                         explored-tile-color
                         black)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles?
            white
            base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              white))))))

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
