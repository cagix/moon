(ns cdq.ui.editor.widget.sound
  (:require [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.widget :as widget]
            [gdl.audio :as audio]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [gdl.ui.group :as group]
            [gdl.ui.stage :as stage]
            [gdl.ui.table :as table]))

(defn- play-button [sound-name]
  (ui/text-button "play!"
                  (fn [_actor {:keys [ctx/audio]}]
                    (audio/play-sound! audio sound-name))))

(declare columns)

(defn- open-choose-sound-window! [table
                                  {:keys [ctx/audio
                                          ctx/stage
                                          ctx/graphics]}]
  (let [rows (for [sound-name (audio/all-sounds audio)]
               [(ui/text-button sound-name
                                (fn [actor _ctx]
                                  (group/clear-children! table)
                                  (table/add-rows! table [(columns table sound-name)])
                                  (.remove (actor/find-ancestor-window actor))
                                  (actor/pack-ancestor-window! table)
                                  (let [[k _] (actor/user-object table)]
                                    (actor/set-user-object! table [k sound-name]))))
                (play-button sound-name)])]
    (stage/add! stage (scroll-pane/choose-window (:width (:ui-viewport graphics))
                                                     rows))))

(defn- columns [table sound-name]
  [(ui/text-button sound-name
                   (fn [_actor ctx]
                     (open-choose-sound-window! table ctx)))
   (play-button sound-name)])

(defmethod widget/create :s/sound [_  _attribute sound-name _ctx]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (table/add-rows! table [(if sound-name
                              (columns table sound-name)
                              [(ui/text-button "No sound"
                                               (fn [_actor ctx]
                                                 (open-choose-sound-window! table ctx)))])])
    table))
