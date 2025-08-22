package org.qpeek.qpeek.common.persistence;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;

@MappedSuperclass
@SequenceGenerator(name = "global_seq_gen", sequenceName = "global_sequence", allocationSize = 50)
public abstract class GlobalSequenceGenerators {
}