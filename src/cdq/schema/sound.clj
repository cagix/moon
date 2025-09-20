(ns cdq.schema.sound
  (:require [cdq.audio :as audio]
            [cdq.stage]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.group :as group]
            [gdl.scene2d.stage :as stage]
            [gdl.scene2d.ui.table :as table]
            [gdl.scene2d.ui.widget-group :as widget-group]
            [gdl.scene2d.ui.window :as window]))

(defn malli-form [_ _schemas]
  :string)

(defn create-value [_ v _db]
  v)

(declare sound-columns)

(defn- rebuild! [table sound-name]
  (fn [actor _ctx]
    (group/clear-children! table)
    (table/add-rows! table [(sound-columns table sound-name)])
    (actor/remove! (window/find-ancestor actor))
    (widget-group/pack! (window/find-ancestor table))
    (let [[k _] (actor/user-object table)]
      (actor/set-user-object! table [k sound-name]))))

(defn- open-select-sounds-handler [table]
  (fn [_actor {:keys [ctx/audio
                      ctx/stage]}]
    (stage/add! stage
                (scene2d/build
                 {:actor/type :actor.type/scroll-pane-window
                  :viewport-height (cdq.stage/viewport-width stage)
                  :rows (for [sound-name (audio/all-sounds audio)]
                          [{:actor {:actor/type :actor.type/text-button
                                    :text sound-name
                                    :on-clicked (rebuild! table sound-name)}}
                           {:actor {:actor/type :actor.type/text-button
                                    :text "play!"
                                    :on-clicked (fn [_actor {:keys [ctx/audio]}]
                                                  (audio/play-sound! audio sound-name))}}])}))))

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
