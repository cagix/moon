(ns forge.mapgen-test
  (:require [cdq.create.level :refer [generate-level]]
            gdl.graphics
            cdq.error
            [cdq.db :as db]
            [cdq.modules :as modules]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.camera :as cam]
            [cdq.ui :refer [ui-actor text-button] :as ui]
            [gdl.gdx.input :as input]
            [gdl.gdx.tiled :as tiled]
            [gdl.gdx.scenes.scene2d.group :refer [add-actor!]]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]])
  (:import (com.badlogic.gdx.graphics Color)))

(def state (atom nil))

(defn- show-whole-map! [camera tiled-map]
  (cam/set-position camera
                    [(/ (tiled/tm-width  tiled-map) 2)
                     (/ (tiled/tm-height tiled-map) 2)])
  (cam/set-zoom camera
                (cam/calculate-zoom camera
                                    :left [0 0]
                                    :top [0 (tiled/tm-height tiled-map)]
                                    :right [(tiled/tm-width tiled-map) 0]
                                    :bottom [0 0])))

(defn- current-data [] ; TODO just use vars
  #_(-> (screen/current)
        :sub-screen
        :current-data))

(def ^:private infotext
  "L: grid lines
  M: movement properties
  zoom: minus,equals
  ESCAPE: leave
  direction keys: move")

(defn- map-infos ^String [{:keys [gdl.graphics/world-viewport] :as c}]
  (let [tile (mapv int (gdl.graphics/world-mouse-position world-viewport))
        {:keys [tiled-map
                area-level-grid]} @(current-data)]
    (->> [infotext
          (str "Tile " tile)
          (when-not area-level-grid
            (str "Module " (mapv (comp int /)
                                 (gdl.graphics/world-mouse-position world-viewport)
                                 [modules/width modules/height])))
          (when area-level-grid
            (str "Creature id: " (tiled/property-value tiled-map :creatures tile :id)))
          (when area-level-grid
            (let [level (get area-level-grid tile)]
              (when (number? level)
                (str "Area level:" level))))
          (str "Movement properties " (tiled/movement-property tiled-map tile) "\n"
               (apply vector (tiled/movement-properties tiled-map tile)))]
         (remove nil?)
         (str/join "\n"))))

(defn- ->info-window [{:keys [gdl.graphics/ui-viewport] :as c}]
  (let [label (ui/label "")
        window (ui/window {:title "Info" :rows [[label]]})]
    (add-actor! window (ui-actor {:act #(do
                                         (.setText label (map-infos %))
                                         (.pack window))}))
    (.setPosition window 0 (:height ui-viewport)) window))

(def ^:private camera-movement-speed 1)

; TODO textfield takes control !
; PLUS symbol shift & = symbol on keyboard not registered
(defn- camera-controls [input camera]
  (let [apply-position (fn [idx f]
                         (cam/set-position camera
                                           (update (cam/position camera)
                                                   idx
                                                   #(f % camera-movement-speed))))]
    (if (input/key-pressed? input :left)  (apply-position 0 -))
    (if (input/key-pressed? input :right) (apply-position 0 +))
    (if (input/key-pressed? input :up)    (apply-position 1 +))
    (if (input/key-pressed? input :down)  (apply-position 1 -))))

(defn- render-on-map [{:keys [gdl.graphics/world-viewport
                              gdl.graphics/shape-drawer]}]
  (let [{:keys [tiled-map
                area-level-grid
                start-position
                show-movement-properties
                show-grid-lines]} @(current-data)
        visible-tiles (cam/visible-tiles (:camera world-viewport))
        [x y] (mapv int (gdl.graphics/world-mouse-position world-viewport))
        sd shape-drawer]
    (sd/rectangle sd x y 1 1 :white)
    (when start-position
      (sd/filled-rectangle sd (start-position 0) (start-position 1) 1 1 [1 0 1 0.9]))
    (when show-movement-properties
      (doseq [[x y] visible-tiles
              :let [prop (tiled/movement-property tiled-map [x y])]]
        (sd/filled-circle sd [(+ x 0.5) (+ y 0.5)] 0.08 :black)
        (sd/filled-circle sd [(+ x 0.5) (+ y 0.5)]
                         0.05
                         (case prop
                           "all"   :green
                           "air"   :orange
                           "none"  :red))))
    (when show-grid-lines
      (sd/grid sd
              0
              0
              (tiled/tm-width  tiled-map)
              (tiled/tm-height tiled-map) 1 1 [1 1 1 0.5]))))

(def ^:private world-id :worlds/uf-caves)

(defn- generate-screen-ctx [{:keys [gdl.graphics/world-viewport
                                    cdq/db]
                             :as c}
                            properties]
  (let [{:keys [tiled-map start-position]} (generate-level c
                                                           (db/build db world-id c))
        atom-data (current-data)]
    (tiled/dispose (:tiled-map @atom-data))
    (swap! atom-data assoc
           :tiled-map tiled-map
           ;:area-level-grid area-level-grid
           :start-position start-position)
    (show-whole-map! (:camera world-viewport) tiled-map)
    (tiled/set-visible (tiled/get-layer tiled-map "creatures") true)))

(defn ->generate-map-window [{:keys [cdq/db] :as c} level-id]
  (ui/window {:title "Properties"
              :cell-defaults {:pad 10}
              :rows [[(ui/label (with-out-str (pprint (db/build db level-id c))))]
                     [(text-button "Generate" #(try (generate-screen-ctx c (db/build db level-id c))
                                                    (catch Throwable t
                                                      (cdq.error/error-window @state t)
                                                      (println t))))]]
              :pack? true}))

(def ^:private zoom-speed 0.025)

(defn adjust-zoom [input camera] ; TODO this now in cdq.context available.
  (when (input/key-pressed? input :minus)  (cam/inc-zoom camera    zoom-speed))
  (when (input/key-pressed? input :equals) (cam/inc-zoom camera (- zoom-speed))))

(defn enter [_]
  #_(show-whole-map! c/camera (:tiled-map @current-data)))

(defn exit [_]
  #_(cam/reset-zoom! c/camera))

(defn render [_]
  #_(gdl.graphics.tiled-map-renderer/draw @state
                                              (:tiled-map @current-data)
                                              (constantly Color/WHITE))
  #_(gdl.graphics/draw-on-world-view @state render-on-map)
  #_(if (input/key-just-pressed? :l)
      (swap! current-data update :show-grid-lines not))
  #_(if (input/key-just-pressed? :m)
      (swap! current-data update :show-movement-properties not))
  #_(adjust-zoom input c/camera)
  #_(camera-controls input c/camera))

#_(defn dispose [_]
  #_(dispose (:tiled-map @current-data)))

(comment
 (atom {:tiled-map (tiled/load-map modules/file)
        :show-movement-properties false
        :show-grid-lines false})
 )

#_(defn actors [_]
    [(->generate-map-window c world-id)
     (->info-window c)])
