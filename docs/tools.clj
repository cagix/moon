(comment
 ; call in component entity/state namespace to get all methods etc.
 (spit "new_entity_methods.clj"
       (with-out-str
        (clojure.pprint/pprint
         (for [[sym var] (ns-publics *ns*)
               :when (instance? clojure.lang.MultiFn @var)]
           [sym (sort (map first (methods @var)))])))))
