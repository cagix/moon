(ns moon.world
  (:require [gdl.graphics.camera :as cam]
            [gdl.utils :refer [dispose]]
            [moon.component :as component]
            [moon.graphics.world-view :as world-view]
            [moon.world.raycaster :as raycaster]
            [moon.world.time :as world.time]))

(declare paused?

         ^{:doc "The game logic update delta-time. Different then gdl.graphics/delta-time because it is bounded by a maximum value for entity movement speed."}
         delta-time

         ^{:doc "The game-logic frame number, starting with 1. (not counting when game is paused)"}
         logic-frame)

(declare ^:private raycaster)

(defn ray-blocked? [start target]
  (raycaster/blocked? raycaster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [start target path-w]
  (raycaster/path-blocked? raycaster start target path-w))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam/position (world-view/camera))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (world-view/width))  2)))
     (<= ydist (inc (/ (float (world-view/height)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? target))
       (not (and los-checks?
                 (ray-blocked? (:position source) (:position target))))))

(def mouseover-eid nil)

(defn mouseover-entity []
  (when-let [eid mouseover-eid]
    @eid))

(declare tiled-map)

(defn clear-tiled-map []
  (when (bound? #'tiled-map)
    (dispose tiled-map)))

(declare explored-tile-corners)
(declare entity-tick-error)
