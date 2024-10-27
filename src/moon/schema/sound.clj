(ns moon.schema.sound
  (:require [clojure.string :as str]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.assets :as assets]
            [moon.schema :as schema]
            [moon.stage :as stage]
            [moon.widgets.scrollpane :refer [scrollable-choose-window]])
  (:import (com.badlogic.gdx.audio Sound)))

(defmethod schema/form :s/sound [_]
  :string)

(defn- play-button [sound-file]
  (ui/text-button "play!" #(assets/play-sound! sound-file)))

(declare columns)

(defn- choose-window [table]
  (let [rows (for [sound-file (assets/all-of-class Sound)]
               [(ui/text-button (str/replace-first sound-file "sounds/" "")
                                (fn []
                                  (ui/clear-children! table)
                                  (ui/add-rows! table [(columns table sound-file)])
                                  (a/remove! (ui/find-ancestor-window ui/*on-clicked-actor*))
                                  (ui/pack-ancestor-window! table)
                                  (let [[k _] (.getUserObject table)]
                                    (.setUserObject table [k sound-file]))))
                (play-button sound-file)])]
    (stage/add! (scrollable-choose-window rows))))

(defn- columns [table sound-file]
  [(ui/text-button (name sound-file) #(choose-window table))
   (play-button sound-file)])

(defmethod schema/widget :s/sound [_ sound-file]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-file
                           (columns table sound-file)
                           [(ui/text-button "No sound" #(choose-window table))])])
    table))
