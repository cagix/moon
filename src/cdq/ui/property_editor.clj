(ns cdq.ui.property-editor
  (:require [cdq.db :as db]
            [cdq.editor.scroll-pane :as scroll-pane]
            [cdq.editor.widget :as widget]
            [cdq.stacktrace :as stacktrace]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.group :as group]
            [cdq.ui.stage :as stage]
            [cdq.ui.table :as table]
            [cdq.ui.text-button :as text-button]
            [cdq.ui.window :as window]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]))

(defn create
  [{:keys [application-state-atom
           schema
           scrollpane-height
           props
           widget]}]
  (let [window (window/create {:title (str "[SKY]Property[]")
                               :id :property-editor-window
                               :modal? true
                               :close-button? true
                               :center? true
                               :close-on-escape? true
                               :cell-defaults {:pad 5}})
        apply-context-fn (fn [window f]
                           (fn [{:keys [ctx/stage] :as ctx}]
                             (try (f ctx)
                                  (actor/remove! window)
                                  (catch Throwable t
                                    (stacktrace/pretty-print t)
                                    (stage/add! stage (error-window/create t))))))
        save!   (apply-context-fn window (fn [{:keys [ctx/db]}]
                                           (swap! application-state-atom update :ctx/db
                                                  db/update!
                                                  (widget/value schema nil widget (:schemas db)))))
        delete! (apply-context-fn window (fn [_ctx]
                                           (swap! application-state-atom update :ctx/db
                                                  db/delete!
                                                  (:property/id props))))
        check-enter-to-save (fn [_actor _delta {:keys [ctx/input] :as ctx}]
                              (when (input/key-just-pressed? input :enter)
                                (save! ctx))) ]
    (table/add-rows! window [[(scroll-pane/table-cell scrollpane-height
                                                      [[{:actor widget :colspan 2}]
                                                       [{:actor (text-button/create "Save [LIGHT_GRAY](ENTER)[]"
                                                                                    (fn [_actor ctx]
                                                                                      (save! ctx)))
                                                         :center? true}
                                                        {:actor (text-button/create "Delete"
                                                                                    (fn [_actor ctx]
                                                                                      (delete! ctx)))
                                                         :center? true}]])]])
    (group/add! window {:actor/type :actor.type/actor
                        :act check-enter-to-save})
    (.pack window)
    window))
