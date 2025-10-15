(ns cdq.ui.editor.schemas-impl)

(def fn-map
  '{
    :s/enum {cdq.ui.editor.schema/create       cdq.ui.editor.schema.enum/create
             cdq.ui.editor.schema/value        cdq.ui.editor.schema.enum/value}

    :s/image {cdq.ui.editor.schema/create       cdq.ui.editor.schema.image/create}

    :s/map {cdq.ui.editor.schema/create       cdq.ui.editor.schema.map/create
            cdq.ui.editor.schema/value        cdq.ui.editor.schema.map/value}

    :s/number {cdq.ui.editor.schema/create       cdq.ui.editor.widget.edn/create
               cdq.ui.editor.schema/value        cdq.ui.editor.widget.edn/value}

    :s/one-to-many {cdq.ui.editor.schema/create       cdq.ui.editor.schema.one-to-many/create
                    cdq.ui.editor.schema/value        cdq.ui.editor.schema.one-to-many/value}

    :s/one-to-one {cdq.ui.editor.schema/create       cdq.ui.editor.schema.one-to-one/create
                   cdq.ui.editor.schema/value        cdq.ui.editor.schema.one-to-one/value}

    :s/sound {cdq.ui.editor.schema/create       cdq.ui.editor.schema.sound/create}

    :s/string {cdq.ui.editor.schema/create       cdq.ui.editor.schema.string/create
               cdq.ui.editor.schema/value        cdq.ui.editor.schema.string/value}

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
