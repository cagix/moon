(ns cdq.ui.editor
  (:require [cdq.application :as application]
            [cdq.ui.editor.overview-table]
            [cdq.stacktrace :as stacktrace]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.widget :as widget]
            [gdl.db :as db]
            [gdl.input :as input]
            [gdl.property :as property]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]))

(defn- apply-context-fn [window f]
  (fn [{:keys [ctx/stage] :as ctx}]
    (try (f ctx)
         (ui/remove! window)
         (catch Throwable t
           (stacktrace/pretty-pst t)
           (stage/add! stage (error-window/create t))))))

; We are working with raw property data without edn->value and build
; otherwise at update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn editor-window [props {:keys [ctx/db
                                   ctx/ui-viewport]
                            :as ctx}]
  (let [schema (get (:schemas db) (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (widget/create schema props ctx)
        save!   (apply-context-fn window (fn [{:keys [ctx/db]}]
                                           (swap! application/state update :ctx/db
                                                  db/update!
                                                  (widget/value schema widget (:schemas db)))))
        delete! (apply-context-fn window (fn [_ctx]
                                           (swap! application/state update :ctx/db
                                                  db/delete!
                                                  (:property/id props))))]
    (ui/add-rows! window [[(scroll-pane/table-cell (:height ui-viewport)
                                                   [[{:actor widget :colspan 2}]
                                                    [{:actor (ui/text-button "Save [LIGHT_GRAY](ENTER)[]"
                                                                             (fn [_actor ctx]
                                                                               (save! ctx)))
                                                      :center? true}
                                                     {:actor (ui/text-button "Delete"
                                                                             (fn [_actor ctx]
                                                                               (delete! ctx)))
                                                      :center? true}]])]])
    (.addActor window (ui/actor {:act (fn [_this _delta {:keys [ctx/input]}]
                                        (when (input/key-just-pressed? input :enter)
                                          (save! ctx)))}))
    (.pack window)
    window))

(defn- edit-property [id {:keys [ctx/db
                                 ctx/stage]
                          :as ctx}]
  (stage/add! stage (editor-window (db/get-raw db id) ctx)))

(defn open-editor-window! [{:keys [ctx/stage] :as ctx} property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})]
    (ui/add! window (cdq.ui.editor.overview-table/create ctx property-type edit-property))
    (.pack window)
    (stage/add! stage window)))
