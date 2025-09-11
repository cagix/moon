(ns cdq.editor.widgets)

(def k->methods*
  '{:s/map {:create cdq.schema.map/create
            :value cdq.schema.map/value}
    :default {:create cdq.editor.widget.default/create
              :value cdq.editor.widget.default/value}
    :widget/edn {:create cdq.editor.widget.edn/create
                 :value cdq.editor.widget.edn/value}
    :s/string {:create cdq.schema.string/create
             :value cdq.schema.string/value}
    :s/boolean {:create cdq.schema.boolean/create
              :value cdq.schema.boolean/value}
    :s/enum {:create cdq.schema.enum/create
           :value cdq.schema.enum/value}
    :s/sound {:create cdq.schema.sound/create}
    :s/one-to-one {:create cdq.schema.one-to-one/create
                   :value cdq.schema.one-to-one/value}
    :s/one-to-many {:create cdq.schema.one-to-many/create
                    :value cdq.schema.one-to-many/value}
    :s/image     {:create cdq.schema.image/create}
    :s/animation {:create cdq.schema.animation/create}})

(require 'cdq.effects)

(def k->methods
  (cdq.effects/walk-method-map k->methods*))
