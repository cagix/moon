(ns cdq.ui.editor.widget.sound
  (:require [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.widget :as widget]
            [gdl.audio :as audio]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]))

(defn- play-button [sound-name]
  (ui/text-button "play!"
                  (fn [_actor {:keys [ctx/audio]}]
                    (audio/play-sound! audio sound-name))))

(declare columns)

(defn- open-choose-sound-window! [table
                                  {:keys [ctx/audio
                                          ctx/stage
                                          ctx/ui-viewport]}]
  (let [rows (for [sound-name (audio/all-sounds audio)]
               [(ui/text-button sound-name
                                (fn [actor _ctx]
                                  (ui/clear-children! table)
                                  (ui/add-rows! table [(columns table sound-name)])
                                  (.remove (ui/find-ancestor-window actor))
                                  (ui/pack-ancestor-window! table)
                                  (let [[k _] (ui/user-object table)]
                                    (ui/set-user-object! table [k sound-name]))))
                (play-button sound-name)])]
    (stage/add! stage (scroll-pane/choose-window (:width ui-viewport)
                                                     rows))))

(defn- columns [table sound-name]
  [(ui/text-button sound-name
                   (fn [_actor ctx]
                     (open-choose-sound-window! table ctx)))
   (play-button sound-name)])

(defmethod widget/create :s/sound [_ sound-name _ctx]
  (let [table (ui/table {:cell-defaults {:pad 5}})]
    (ui/add-rows! table [(if sound-name
                           (columns table sound-name)
                           [(ui/text-button "No sound"
                                            (fn [_actor ctx]
                                              (open-choose-sound-window! table ctx)))])])
    table))
