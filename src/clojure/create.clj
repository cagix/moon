(ns clojure.create
  (:require [clojure.grid2d :as g2d]
            [clojure.tiled :as tiled]))

(defn explored-tile-corners* [{:keys [clojure.context/tiled-map]}]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))

(defn error* [_context]
  nil)

(defn tiled-map* [context]
  (:tiled-map (:clojure.context/level context)))

(defn entity-ids* [_context]
  (atom {}))

(defn factions-iterations* [config _context]
  config)
