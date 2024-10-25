(ns moon.widgets.property
  (:require [gdl.input :refer [key-just-pressed?]]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.component :refer [defc] :as component]
            [moon.db :as db]
            [moon.property :as property]
            [moon.schema :as schema]
            [moon.widgets.scrollpane :refer [scroll-pane-cell]]
            [moon.widgets.error-window :refer [error-window!]]))

(defn- apply-context-fn [window f]
  #(try (f)
        (a/remove! window)
        (catch Throwable t
          (error-window! t))))

; We are working with raw property data without edn->value and db/build
; otherwise at db/update! we would have to convert again from edn->value back to edn
; for example at images/relationships
(defn- props->editor-window [props]
  (let [schema (schema/of (property/type props))
        window (ui/window {:title (str "[SKY]Property[]")
                           :id :property-editor-window
                           :modal? true
                           :close-button? true
                           :center? true
                           :close-on-escape? true
                           :cell-defaults {:pad 5}})
        widget (schema/widget schema props)
        save!   (apply-context-fn window #(db/update! (schema/widget-value schema widget)))
        delete! (apply-context-fn window #(db/delete! (:property/id props)))]
    (ui/add-rows! window [[(scroll-pane-cell [[{:actor widget :colspan 2}]
                                              [{:actor (ui/text-button "Save [LIGHT_GRAY](ENTER)[]" save!)
                                                :center? true}
                                               {:actor (ui/text-button "Delete" delete!)
                                                :center? true}]])]])
    (ui/add-actor! window (ui/actor {:act (fn []
                                            (when (key-just-pressed? :enter)
                                              (save!)))}))
    (.pack window)
    window))

(defc :widgets/property-editor
  (component/create [[_ props]]
    (props->editor-window props)))
