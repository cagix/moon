(ns cdq.ui.editor.schemas-impl)

(def fn-map
  '{
    :s/number {cdq.ui.editor.schema/create       cdq.ui.editor.widget.edn/create
               cdq.ui.editor.schema/value        cdq.ui.editor.widget.edn/value}

    :s/val-max {cdq.ui.editor.schema/create       cdq.ui.editor.widget.edn/create
                cdq.ui.editor.schema/value        cdq.ui.editor.widget.edn/value}

    }
  )

(doseq [[schema-k impls] fn-map
        [multifn-sym impl-fn] impls
        :let [multifn @(requiring-resolve multifn-sym)
              method-var (requiring-resolve impl-fn)]]
  (clojure.lang.MultiFn/.addMethod multifn
                                   schema-k
                                   method-var))
