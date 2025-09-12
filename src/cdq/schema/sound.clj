(ns cdq.schema.sound
  (:require [cdq.ctx.audio :as audio]
            [cdq.stage]
            [cdq.ui.widget]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.table :as table]
            [clojure.gdx.scenes.scene2d.ui.window :as window]
            [clojure.vis-ui.widget :as widget]))

(defn malli-form [_ _schemas]
  :string)

(declare sound-columns) ; < -- rebuilding itself ....

(defn- rebuild! [table sound-name]
  (fn [actor _ctx]
    ; TODO just rebuild the editor window ?

    (group/clear-children! table)

    (table/add-rows! table [(sound-columns table sound-name)])

    (.remove (window/find-ancestor actor))

    (window/pack-ancestors! table)

    (let [[k _] (actor/user-object table)]
      (actor/set-user-object! table [k sound-name]))
    ))

(defn- open-select-sounds-handler [table]
  (fn [_actor {:keys [ctx/audio
                      ctx/stage]}]
    (stage/add! stage
                (cdq.ui.widget/scroll-pane-window
                 (cdq.stage/viewport-width stage)
                 (for [sound-name (audio/all-sounds audio)]
                   [(widget/text-button sound-name (rebuild! table sound-name))
                    (widget/text-button "play!"
                                        (fn [_actor {:keys [ctx/audio]}]
                                          (audio/play-sound! audio sound-name)))])))))

(defn- sound-columns [table sound-name]
  [(widget/text-button sound-name (open-select-sounds-handler table))
   (widget/text-button "play!"
                       (fn [_actor {:keys [ctx/audio]}]
                         (audio/play-sound! audio sound-name)))])

(defn create [_  sound-name _ctx]
  (let [table (widget/table {:cell-defaults {:pad 5}})]
    (table/add-rows! table [(if sound-name
                              (sound-columns table sound-name)
                              [(widget/text-button "No sound" (open-select-sounds-handler table))])])
    table))
