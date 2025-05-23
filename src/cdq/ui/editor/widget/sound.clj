(ns cdq.ui.editor.widget.sound
  (:require [cdq.g :as g]
            [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.widget :as widget]
            [clojure.string :as str]
            [gdl.ui :as ui]))

(defn- play-button [sound-name]
  (ui/text-button "play!"
                  (fn [_actor ctx]
                    (g/handle-txs! ctx [[:tx/sound sound-name]]))))

(declare columns)

(defn- sound-file->sound-name [sound-file]
  (-> sound-file
      (str/replace-first "sounds/" "")
      (str/replace ".wav" "")))

(defn- open-choose-sound-window! [table ctx]
  (let [rows (for [sound-name (map sound-file->sound-name (g/all-sounds ctx))]
               [(ui/text-button sound-name
                                (fn [actor _ctx]
                                  (ui/clear-children! table)
                                  (ui/add-rows! table [(columns table sound-name)])
                                  (.remove (ui/find-ancestor-window actor))
                                  (ui/pack-ancestor-window! table)
                                  (let [[k _] (ui/user-object table)]
                                    (ui/set-user-object! table [k sound-name]))))
                (play-button sound-name)])]
    (g/add-actor! ctx (scroll-pane/choose-window (g/ui-viewport-width ctx)
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
