(ns cdq.schema.sound
  (:require [cdq.ctx.audio :as audio]
            [cdq.ctx.stage]
            [cdq.ui.widget]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.scene2d.ui.table :as table]
            [clojure.gdx.scene2d.ui.window :as window]))

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
                 (cdq.ctx.stage/viewport-width stage)
                 (for [sound-name (audio/all-sounds audio)]
                   [{:actor {:actor/type :actor.type/text-button
                             :text sound-name
                             :on-clicked (rebuild! table sound-name)}}
                    {:actor {:actor/type :actor.type/text-button
                             :text "play!"
                             :on-clicked (fn [_actor {:keys [ctx/audio]}]
                                           (audio/play-sound! audio sound-name))}}])))))

(defn- sound-columns [table sound-name]
  [{:actor {:actor/type :actor.type/text-button
            :text sound-name
            :on-clicked (open-select-sounds-handler table)}}
   {:actor {:actor/type :actor.type/text-button
            :text "play!"
            :on-clicked (fn [_actor {:keys [ctx/audio]}]
                          (audio/play-sound! audio sound-name))}}])

(defn create [_  sound-name _ctx]
  (let [table (scene2d/build
               {:actor/type :actor.type/table
                :cell-defaults {:pad 5}})]
    (table/add-rows! table [(if sound-name
                              (sound-columns table sound-name)
                              [{:actor {:actor/type :actor.type/text-button
                                        :text "No sound"
                                        :on-clicked (open-select-sounds-handler table)}}])])
    table))
