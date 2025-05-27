(ns cdq.tile-color-setter
  (:require [cdq.raycaster :as raycaster]
            [gdl.graphics.color :as color]))

(defn create [{:keys [raycaster
                      explored-tile-corners
                      light-position
                      explored-tile-color
                      see-all-tiles?]}]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored?
                         explored-tile-color
                         color/black)
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
            color/white
            base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              color/white))))))

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
