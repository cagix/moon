(ns cdq.ui.editor
  (:require [cdq.application :as application]
            [cdq.db :as db]
            [cdq.g :as g]
            [cdq.property :as property]
            [cdq.stacktrace :as stacktrace]
            [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.overview-table :as overview-table]
            [cdq.ui.editor.widget :as widget]
            [gdl.ui :as ui]))

(defn- update-property! [ctx property]
  (update ctx :ctx/db db/update! property))

(defn- delete-property! [ctx property-id]
  (update ctx :ctx/db db/delete! property-id))

(defn- apply-context-fn [window f]
  (fn [ctx]
    (try (f ctx)
         (ui/remove! window)
         (catch Throwable t
           (stacktrace/pretty-pst t)
           (g/open-error-window! ctx t)))))

; We are working with raw property data without edn->value and build
; otherwise at update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn editor-window [props {:keys [ctx/db] :as ctx}]
  (let [schema (get (:schemas db) (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (widget/create schema props ctx)
        save!   (apply-context-fn window (fn [ctx]
                                           (swap! application/state
                                                  update-property!
                                                  (widget/value schema widget (:schemas db)))))
        delete! (apply-context-fn window (fn [_ctx]
                                           (swap! application/state
                                                  delete-property!
                                                  (:property/id props))))]
    (ui/add-rows! window [[(scroll-pane/table-cell (g/ui-viewport-height ctx)
                                                   [[{:actor widget :colspan 2}]
                                                    [{:actor (ui/text-button "Save [LIGHT_GRAY](ENTER)[]"
                                                                             (fn [_actor ctx]
                                                                               (save! ctx)))
                                                      :center? true}
                                                     {:actor (ui/text-button "Delete"
                                                                             (fn [_actor ctx]
                                                                               (delete! ctx)))
                                                      :center? true}]])]])
    (.addActor window (ui/actor {:act (fn [_this _delta ctx]
                                        (when (g/key-just-pressed? ctx :enter)
                                          (save! ctx)))}))
    (.pack window)
    window))

(defn- edit-property [id {:keys [ctx/db] :as ctx}]
  (g/add-actor! ctx (editor-window (db/get-raw db id) ctx)))

(defn open-editor-window! [ctx property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})]
    (ui/add! window (overview-table/create ctx property-type edit-property))
    (.pack window)
    (g/add-actor! ctx window)))
