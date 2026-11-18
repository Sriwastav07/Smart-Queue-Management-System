export type Role = 'USER' | 'STAFF' | 'ADMIN' | 'SUPER_ADMIN';

export type TokenStatus = 'WAITING' | 'CALLED' | 'SERVING' | 'COMPLETED' | 'SKIPPED' | 'CANCELLED' | 'NO_SHOW' | 'TRANSFERRED';

export type CounterStatus = 'OPEN' | 'PAUSED' | 'CLOSED';

export type NotificationType =
  | 'QUEUE_JOINED'
  | 'QUEUE_POSITION_CHANGED'
  | 'APPROACHING_TURN'
  | 'TOKEN_CALLED'
  | 'TOKEN_COMPLETED'
  | 'TOKEN_SKIPPED'
  | 'QUEUE_CANCELLED'
  | 'COUNTER_PAUSED'
  | 'COUNTER_RESUMED';

export interface User {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string;
  role: Role;
  organizationId?: number;
  organizationName?: string;
  organizationCategory?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: User;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errorCode?: string;
  timestamp?: string;
}

export interface Organization {
  id: number;
  name: string;
  category: string;
  description?: string;
  address?: string;
  city?: string;
  contactEmail?: string;
  contactPhone?: string;
  active: boolean;
  totalDepartmentsCount?: number;
  activeQueueCount?: number;
  estimatedWaitMinutes?: number;
}

export interface Department {
  id: number;
  name: string;
  description?: string;
  organizationId: number;
  organizationName?: string;
}

export interface ServiceItem {
  id: number;
  name: string;
  description?: string;
  prefix: string;
  defaultEstimatedDurationMinutes: number;
  departmentId: number;
  departmentName?: string;
  organizationId: number;
  organizationName?: string;
  active: boolean;
  queueId?: number;
  currentWaitingCount?: number;
  estimatedWaitMinutes?: number;
}

export interface QueueToken {
  id: number;
  tokenNumber: string;
  sequenceNumber: number;
  queueId: number;
  serviceId: number;
  serviceName: string;
  departmentId: number;
  departmentName: string;
  organizationId: number;
  organizationName: string;
  userId: number;
  userName: string;
  status: TokenStatus;
  counterId?: number;
  counterName?: string;
  counterNumber?: string;
  peopleAhead: number;
  estimatedWaitMinutes: number;
  notifyThreshold: number;
  notificationSent: boolean;
  calledAt?: string;
  serviceStartedAt?: string;
  completedAt?: string;
  actualServiceDurationSeconds?: number;
  createdAt: string;
}

export interface Counter {
  id: number;
  name: string;
  counterNumber: string;
  departmentId: number;
  departmentName?: string;
  status: CounterStatus;
  assignedStaffId?: number;
  assignedStaffName?: string;
  currentToken?: Partial<QueueToken>;
}

export interface NotificationItem {
  id: number;
  userId: number;
  title: string;
  message: string;
  type: NotificationType;
  read: boolean;
  queueTokenId?: number;
  createdAt: string;
}

export interface QueuePrediction {
  serviceId: number;
  serviceName: string;
  dayOfWeek: string;
  hourOfDay: number;
  expectedQueueSize: number;
  expectedWaitTimeMinutes: number;
  recommendation: string;
  hourlyForecasts: {
    timeSlot: string;
    expectedCrowd: number;
    expectedWaitMinutes: number;
    crowdLevel: string;
  }[];
}

export interface AdminAnalytics {
  organizationId: number;
  organizationName: string;
  totalCustomersToday: number;
  currentlyWaiting: number;
  averageWaitMinutes: number;
  completedToday: number;
  noShowsToday: number;
  cancelledToday: number;
  activeCountersCount: number;
  totalCountersCount: number;
  hourlyQueueStats: { hourLabel: string; joinedCount: number; servedCount: number }[];
  serviceWiseDemand: { serviceName: string; count: number; averageDurationMinutes: number }[];
  counterPerformance: { counterName: string; staffName: string; tokensServed: number; avgServiceDurationSeconds: number }[];
}
