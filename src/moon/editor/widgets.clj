(ns ^:no-doc moon.editor.widgets
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [gdl.assets :as gdx.assets]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [gdl.utils :refer [truncate ->edn-str]]
            [moon.component :as component]
            [moon.db :as db]
            [moon.schema :as schema]
            [moon.property :as property]
            [moon.editor.overview :refer [overview-table]]
            [moon.editor.utils :refer [scrollable-choose-window]]
            [moon.assets :as assets]
            [moon.graphics :as g]
            [moon.stage :as stage])
  (:import (com.kotcrab.vis.ui.widget VisTextField VisCheckBox))
  (:load "widgets_relationships"))

(defn- add-schema-tooltip! [widget schema]
  (ui/add-tooltip! widget (str schema))
  widget)

;;

(defmethod schema/widget :default [_ v]
  (ui/label (truncate (->edn-str v) 60)))

(defmethod schema/widget-value :default [_ widget]
  ((a/id widget) 1))

;;

(defmethod schema/widget :boolean [_ checked?]
  (assert (boolean? checked?))
  (ui/check-box "" (fn [_]) checked?))

(defmethod schema/widget-value :boolean [_ widget]
  (VisCheckBox/.isChecked  widget))

;;

(defmethod schema/widget :string [schema v]
  (add-schema-tooltip! (ui/text-field v {}) schema))

(defmethod schema/widget-value :string [_ widget]
  (VisTextField/.getText widget))

;;

(defmethod schema/widget number? [schema v]
  (add-schema-tooltip! (ui/text-field (->edn-str v) {}) schema))

(defmethod schema/widget-value number? [_ widget]
  (edn/read-string (VisTextField/.getText widget)))

;;

(defmethod schema/widget :enum [schema v]
  (ui/select-box {:items (map ->edn-str (rest schema))
                  :selected (->edn-str v)}))

(defmethod schema/widget-value :enum [_ widget]
  (edn/read-string (.getSelected ^com.kotcrab.vis.ui.widget.VisSelectBox widget)))

;;

(defn- all-textures []
  (gdx.assets/of-class assets/manager com.badlogic.gdx.graphics.Texture))

(defn- all-sounds []
  (gdx.assets/of-class assets/manager com.badlogic.gdx.audio.Sound))

; too many ! too big ! scroll ... only show files first & preview?
; make tree view from folders, etc. .. !! all creatures animations showing...
(defn- texture-rows []
  (for [file (sort (all-textures))]
    [(ui/image-button (g/image file) (fn []))]
    #_[(ui/text-button file (fn []))]))

(defn- big-image-button [image]
  (ui/image-button (g/edn->image image)
                   (fn on-clicked [])
                   {:scale 2}))

(defmethod schema/widget :s/image [_ image]
  (big-image-button image)
  #_(ui/image-button image
                     #(stage/add! (scrollable-choose-window (texture-rows)))
                     {:dimensions [96 96]})) ; x2  , not hardcoded here

;;

(defmethod schema/widget :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (big-image-button image))]
             :cell-defaults {:pad 1}}))

;;


(defn- ->play-sound-button [sound-file]
  (ui/text-button "play!" #(assets/play-sound! sound-file)))

(declare ->sound-columns)

(defn- open-sounds-window! [table]
  (let [rows (for [sound-file (all-sounds)]
               [(ui/text-button (str/replace-first sound-file "sounds/" "")
                                (fn []
                                  (ui/clear-children! table)
                                  (ui/add-rows! table [(->sound-columns table sound-file)])
                                  (a/remove! (ui/find-ancestor-window ui/*on-clicked-actor*))
                                  (ui/pack-ancestor-window! table)
                                  (a/set-id! table sound-file)))
                (->play-sound-button sound-file)])]
    (stage/add! (scrollable-choose-window rows))))

(defn- ->sound-columns [table sound-file]
  [(ui/text-button (name sound-file) #(open-sounds-window! table))
   (->play-sound-button sound-file)])

(defmethod schema/widget :s/sound [_ sound-file]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-file
                           (->sound-columns table sound-file)
                           [(ui/text-button "No sound" #(open-sounds-window! table))])])
    table))
