(ns cdq.ui.editor
  (:require [cdq.application :as application]
            [cdq.editor :as editor]
            [cdq.input :as input]
            [cdq.property :as property]
            [cdq.stacktrace :as stacktrace]
            [cdq.stage :as stage]
            [cdq.ui.editor.scroll-pane :as scroll-pane]
            [cdq.ui.editor.widget :as widget]
            [gdl.db :as db]
            [gdl.ui :as ui]))

(defn- apply-context-fn [window f]
  (fn [ctx]
    (try (f ctx)
         (ui/remove! window)
         (catch Throwable t
           (stacktrace/pretty-pst t)
           (stage/open-error-window! ctx t)))))

; We are working with raw property data without edn->value and build
; otherwise at update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn editor-window [props {:keys [ctx/ui-viewport] :as ctx}]
  (let [schema (get (db/schemas ctx) (property/type props))
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
                                                  db/update-property
                                                  (widget/value schema widget (db/schemas ctx)))))
        delete! (apply-context-fn window (fn [_ctx]
                                           (swap! application/state
                                                  db/delete-property
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
    (.addActor window (ui/actor {:act (fn [_this _delta ctx]
                                        (when (input/key-just-pressed? ctx :enter)
                                          (save! ctx)))}))
    (.pack window)
    window))

(defn- edit-property [id ctx]
  (stage/add-actor! ctx (editor-window (db/get-raw ctx id) ctx)))

(defn open-editor-window! [ctx property-type]
  (let [window (ui/window {:title "Edit"
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true})]
    (ui/add! window (editor/property-overview-table ctx property-type edit-property))
    (.pack window)
    (stage/add-actor! ctx window)))
